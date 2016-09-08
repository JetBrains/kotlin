/**
  ******************************************************************************
  * @file    usb_dcd_int.c
  * @author  MCD Application Team
  * @version V2.0.0
  * @date    22-July-2011
  * @brief   Peripheral Device interrupt subroutines
  ******************************************************************************
  * @attention 
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

#include "usb_conf.h"
#ifdef USE_DEVICE_MODE
/* Includes ------------------------------------------------------------------*/
#include "usb_dcd_int.h"
/** @addtogroup USB_OTG_DRIVER
* @{
*/

/** @defgroup USB_DCD_INT 
* @brief This file contains the interrupt subroutines for the Device mode.
* @{
*/


/** @defgroup USB_DCD_INT_Private_Defines
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_DCD_INT_Private_TypesDefinitions
* @{
*/ 
/**
* @}
*/ 



/** @defgroup USB_DCD_INT_Private_Macros
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_DCD_INT_Private_Variables
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_DCD_INT_Private_FunctionPrototypes
* @{
*/ 
/* static functions */
static uint32_t DCD_ReadDevInEP (USB_OTG_CORE_HANDLE *pdev, uint8_t epnum);

/* Interrupt Handlers */
static uint32_t DCD_HandleInEP_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_HandleOutEP_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_HandleSof_ISR(USB_OTG_CORE_HANDLE *pdev);

static uint32_t DCD_HandleRxStatusQueueLevel_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_WriteEmptyTxFifo(USB_OTG_CORE_HANDLE *pdev , uint32_t epnum);

static uint32_t DCD_HandleUsbReset_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_HandleEnumDone_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_HandleResume_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_HandleUSBSuspend_ISR(USB_OTG_CORE_HANDLE *pdev);

static uint32_t DCD_IsoINIncomplete_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_IsoOUTIncomplete_ISR(USB_OTG_CORE_HANDLE *pdev);
#ifdef VBUS_SENSING_ENABLED
static uint32_t DCD_SessionRequest_ISR(USB_OTG_CORE_HANDLE *pdev);
static uint32_t DCD_OTG_ISR(USB_OTG_CORE_HANDLE *pdev);
#endif

/**
* @}
*/ 


/** @defgroup USB_DCD_INT_Private_Functions
* @{
*/ 


#ifdef USB_OTG_HS_DEDICATED_EP1_ENABLED  
/**
* @brief  USBD_OTG_EP1OUT_ISR_Handler
*         handles all USB Interrupts
* @param  pdev: device instance
* @retval status
*/
uint32_t USBD_OTG_EP1OUT_ISR_Handler (USB_OTG_CORE_HANDLE *pdev)
{
  
  USB_OTG_DOEPINTn_TypeDef  doepint;
  USB_OTG_DEPXFRSIZ_TypeDef  deptsiz;  
  
  doepint.d32 = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[1]->DOEPINT);
  doepint.d32&= USB_OTG_READ_REG32(&pdev->regs.DREGS->DOUTEP1MSK);
  
  /* Transfer complete */
  if ( doepint.b.xfercompl )
  {
    /* Clear the bit in DOEPINTn for this interrupt */
    CLEAR_OUT_EP_INTR(1, xfercompl);
    if (pdev->cfg.dma_enable == 1)
    {
      deptsiz.d32 = USB_OTG_READ_REG32(&(pdev->regs.OUTEP_REGS[1]->DOEPTSIZ));
      /*ToDo : handle more than one single MPS size packet */
      pdev->dev.out_ep[1].xfer_count = pdev->dev.out_ep[1].maxpacket - \
        deptsiz.b.xfersize;
    }    
    /* Inform upper layer: data ready */
    /* RX COMPLETE */
    USBD_DCD_INT_fops->DataOutStage(pdev , 1);
    
  }
  
  /* Endpoint disable  */
  if ( doepint.b.epdisabled )
  {
    /* Clear the bit in DOEPINTn for this interrupt */
    CLEAR_OUT_EP_INTR(1, epdisabled);
  }
  /* AHB Error */
  if ( doepint.b.ahberr )
  {
    CLEAR_OUT_EP_INTR(1, ahberr);
  } 
  return 1;
}

/**
* @brief  USBD_OTG_EP1IN_ISR_Handler
*         handles all USB Interrupts
* @param  pdev: device instance
* @retval status
*/
uint32_t USBD_OTG_EP1IN_ISR_Handler (USB_OTG_CORE_HANDLE *pdev)
{
  
  USB_OTG_DIEPINTn_TypeDef  diepint;
  uint32_t fifoemptymsk, msk, emp;
  
  msk = USB_OTG_READ_REG32(&pdev->regs.DREGS->DINEP1MSK);
  emp = USB_OTG_READ_REG32(&pdev->regs.DREGS->DIEPEMPMSK);
  msk |= ((emp >> 1 ) & 0x1) << 7;
  diepint.d32  = USB_OTG_READ_REG32(&pdev->regs.INEP_REGS[1]->DIEPINT) & msk;  
  
  if ( diepint.b.xfercompl )
  {
    fifoemptymsk = 0x1 << 1;
    USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DIEPEMPMSK, fifoemptymsk, 0);
    CLEAR_IN_EP_INTR(1, xfercompl);
    /* TX COMPLETE */
    USBD_DCD_INT_fops->DataInStage(pdev , 1);
  }
  if ( diepint.b.ahberr )
  {
    CLEAR_IN_EP_INTR(1, ahberr);
  }
  if ( diepint.b.epdisabled )
  {
    CLEAR_IN_EP_INTR(1, epdisabled);
  }  
  if ( diepint.b.timeout )
  {
    CLEAR_IN_EP_INTR(1, timeout);
  }
  if (diepint.b.intktxfemp)
  {
    CLEAR_IN_EP_INTR(1, intktxfemp);
  }
  if (diepint.b.intknepmis)
  {
    CLEAR_IN_EP_INTR(1, intknepmis);
  }
  if (diepint.b.inepnakeff)
  {
    CLEAR_IN_EP_INTR(1, inepnakeff);
  }
  if (diepint.b.emptyintr)
  {
    DCD_WriteEmptyTxFifo(pdev , 1);
    CLEAR_IN_EP_INTR(1, emptyintr);
  }
  return 1;
}
#endif

/**
* @brief  STM32_USBF_OTG_ISR_Handler
*         handles all USB Interrupts
* @param  pdev: device instance
* @retval status
*/
uint32_t USBD_OTG_ISR_Handler (USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  gintr_status;
  uint32_t retval = 0;
  
  if (USB_OTG_IsDeviceMode(pdev)) /* ensure that we are in device mode */
  {
    gintr_status.d32 = USB_OTG_ReadCoreItr(pdev);
    if (!gintr_status.d32) /* avoid spurious interrupt */
    {
      return 0;
    }
    
    if (gintr_status.b.outepintr)
    {
      retval |= DCD_HandleOutEP_ISR(pdev);
    }    
    
    if (gintr_status.b.inepint)
    {
      retval |= DCD_HandleInEP_ISR(pdev);
    }
    
    if (gintr_status.b.modemismatch)
    {
      USB_OTG_GINTSTS_TypeDef  gintsts;
      
      /* Clear interrupt */
      gintsts.d32 = 0;
      gintsts.b.modemismatch = 1;
      USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTSTS, gintsts.d32);
    }
    
    if (gintr_status.b.wkupintr)
    {
      retval |= DCD_HandleResume_ISR(pdev);
    }
    
    if (gintr_status.b.usbsuspend)
    {
      retval |= DCD_HandleUSBSuspend_ISR(pdev);
    }
    if (gintr_status.b.sofintr)
    {
      retval |= DCD_HandleSof_ISR(pdev);
      
    }
    
    if (gintr_status.b.rxstsqlvl)
    {
      retval |= DCD_HandleRxStatusQueueLevel_ISR(pdev);
      
    }
    
    if (gintr_status.b.usbreset)
    {
      retval |= DCD_HandleUsbReset_ISR(pdev);
      
    }
    if (gintr_status.b.enumdone)
    {
      retval |= DCD_HandleEnumDone_ISR(pdev);
    }
    
    if (gintr_status.b.incomplisoin)
    {
      retval |= DCD_IsoINIncomplete_ISR(pdev);
    }

    if (gintr_status.b.incomplisoout)
    {
      retval |= DCD_IsoOUTIncomplete_ISR(pdev);
    }    
#ifdef VBUS_SENSING_ENABLED
    if (gintr_status.b.sessreqintr)
    {
      retval |= DCD_SessionRequest_ISR(pdev);
    }

    if (gintr_status.b.otgintr)
    {
      retval |= DCD_OTG_ISR(pdev);
    }   
#endif    
  }
  return retval;
}

#ifdef VBUS_SENSING_ENABLED
/**
* @brief  DCD_SessionRequest_ISR
*         Indicates that the USB_OTG controller has detected a connection
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_SessionRequest_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  gintsts;  
  USBD_DCD_INT_fops->DevConnected (pdev);

  /* Clear interrupt */
  gintsts.d32 = 0;
  gintsts.b.sessreqintr = 1;
  USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GINTSTS, gintsts.d32);   
  return 1;
}

/**
* @brief  DCD_OTG_ISR
*         Indicates that the USB_OTG controller has detected an OTG event:
*                 used to detect the end of session i.e. disconnection
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_OTG_ISR(USB_OTG_CORE_HANDLE *pdev)
{

  USB_OTG_GOTGINT_TypeDef  gotgint;

  gotgint.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GOTGINT);
  
  if (gotgint.b.sesenddet)
  {
    USBD_DCD_INT_fops->DevDisconnected (pdev);
  }
  /* Clear OTG interrupt */
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GOTGINT, gotgint.d32); 
  return 1;
}
#endif
/**
* @brief  DCD_HandleResume_ISR
*         Indicates that the USB_OTG controller has detected a resume or
*                 remote Wake-up sequence
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleResume_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  gintsts;
  USB_OTG_DCTL_TypeDef     devctl;
  USB_OTG_PCGCCTL_TypeDef  power;
  
  if(pdev->cfg.low_power)
  {
    /* un-gate USB Core clock */
    power.d32 = USB_OTG_READ_REG32(&pdev->regs.PCGCCTL);
    power.b.gatehclk = 0;
    power.b.stoppclk = 0;
    USB_OTG_WRITE_REG32(pdev->regs.PCGCCTL, power.d32);
  }
  
  /* Clear the Remote Wake-up Signaling */
  devctl.d32 = 0;
  devctl.b.rmtwkupsig = 1;
  USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DCTL, devctl.d32, 0);
  
  /* Inform upper layer by the Resume Event */
  USBD_DCD_INT_fops->Resume (pdev);
  
  /* Clear interrupt */
  gintsts.d32 = 0;
  gintsts.b.wkupintr = 1;
  USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GINTSTS, gintsts.d32);
  return 1;
}

/**
* @brief  USB_OTG_HandleUSBSuspend_ISR
*         Indicates that SUSPEND state has been detected on the USB
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleUSBSuspend_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  gintsts;
  USB_OTG_PCGCCTL_TypeDef  power;
  USB_OTG_DSTS_TypeDef     dsts;
  
  USBD_DCD_INT_fops->Suspend (pdev);      
  
  dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
    
  /* Clear interrupt */
  gintsts.d32 = 0;
  gintsts.b.usbsuspend = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTSTS, gintsts.d32);
  
  if((pdev->cfg.low_power) && (dsts.b.suspsts == 1))
  {
	/*  switch-off the clocks */
    power.d32 = 0;
    power.b.stoppclk = 1;
    USB_OTG_MODIFY_REG32(pdev->regs.PCGCCTL, 0, power.d32);  
    
    power.b.gatehclk = 1;
    USB_OTG_MODIFY_REG32(pdev->regs.PCGCCTL, 0, power.d32);
    
    /* Request to enter Sleep mode after exit from current ISR */
    SCB->SCR |= (SCB_SCR_SLEEPDEEP_Msk | SCB_SCR_SLEEPONEXIT_Msk);
  }
  return 1;
}

/**
* @brief  DCD_HandleInEP_ISR
*         Indicates that an IN EP has a pending Interrupt
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleInEP_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_DIEPINTn_TypeDef  diepint;
  
  uint32_t ep_intr;
  uint32_t epnum = 0;
  uint32_t fifoemptymsk;
  diepint.d32 = 0;
  ep_intr = USB_OTG_ReadDevAllInEPItr(pdev);
  
  while ( ep_intr )
  {
    if (ep_intr&0x1) /* In ITR */
    {
      diepint.d32 = DCD_ReadDevInEP(pdev , epnum); /* Get In ITR status */
      if ( diepint.b.xfercompl )
      {
        fifoemptymsk = 0x1 << epnum;
        USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DIEPEMPMSK, fifoemptymsk, 0);
        CLEAR_IN_EP_INTR(epnum, xfercompl);
        /* TX COMPLETE */
        USBD_DCD_INT_fops->DataInStage(pdev , epnum);
        
        if (pdev->cfg.dma_enable == 1)
        {
          if((epnum == 0) && (pdev->dev.device_state == USB_OTG_EP0_STATUS_IN))
          {
            /* prepare to rx more setup packets */
            USB_OTG_EP0_OutStart(pdev);
          }
        }           
      }
      if ( diepint.b.ahberr )
      {
        CLEAR_IN_EP_INTR(epnum, ahberr);
      }
      if ( diepint.b.timeout )
      {
        CLEAR_IN_EP_INTR(epnum, timeout);
      }
      if (diepint.b.intktxfemp)
      {
        CLEAR_IN_EP_INTR(epnum, intktxfemp);
      }
      if (diepint.b.intknepmis)
      {
        CLEAR_IN_EP_INTR(epnum, intknepmis);
      }
      if (diepint.b.inepnakeff)
      {
        CLEAR_IN_EP_INTR(epnum, inepnakeff);
      }
      if ( diepint.b.epdisabled )
      {
        CLEAR_IN_EP_INTR(epnum, epdisabled);
      }       
      if (diepint.b.emptyintr)
      {
        
        DCD_WriteEmptyTxFifo(pdev , epnum);
        
        CLEAR_IN_EP_INTR(epnum, emptyintr);
      }
    }
    epnum++;
    ep_intr >>= 1;
  }
  
  return 1;
}

/**
* @brief  DCD_HandleOutEP_ISR
*         Indicates that an OUT EP has a pending Interrupt
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleOutEP_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  uint32_t ep_intr;
  USB_OTG_DOEPINTn_TypeDef  doepint;
  USB_OTG_DEPXFRSIZ_TypeDef  deptsiz;
  uint32_t epnum = 0;
  
  doepint.d32 = 0;
  
  /* Read in the device interrupt bits */
  ep_intr = USB_OTG_ReadDevAllOutEp_itr(pdev);
  
  while ( ep_intr )
  {
    if (ep_intr&0x1)
    {
      
      doepint.d32 = USB_OTG_ReadDevOutEP_itr(pdev, epnum);
      
      /* Transfer complete */
      if ( doepint.b.xfercompl )
      {
        /* Clear the bit in DOEPINTn for this interrupt */
        CLEAR_OUT_EP_INTR(epnum, xfercompl);
        if (pdev->cfg.dma_enable == 1)
        {
          deptsiz.d32 = USB_OTG_READ_REG32(&(pdev->regs.OUTEP_REGS[epnum]->DOEPTSIZ));
          /*ToDo : handle more than one single MPS size packet */
          pdev->dev.out_ep[epnum].xfer_count = pdev->dev.out_ep[epnum].maxpacket - \
            deptsiz.b.xfersize;
        }
        /* Inform upper layer: data ready */
        /* RX COMPLETE */
        USBD_DCD_INT_fops->DataOutStage(pdev , epnum);
        
        if (pdev->cfg.dma_enable == 1)
        {
          if((epnum == 0) && (pdev->dev.device_state == USB_OTG_EP0_STATUS_OUT))
          {
            /* prepare to rx more setup packets */
            USB_OTG_EP0_OutStart(pdev);
          }
        }        
      }
      /* Endpoint disable  */
      if ( doepint.b.epdisabled )
      {
        /* Clear the bit in DOEPINTn for this interrupt */
        CLEAR_OUT_EP_INTR(epnum, epdisabled);
      }
      /* AHB Error */
      if ( doepint.b.ahberr )
      {
        CLEAR_OUT_EP_INTR(epnum, ahberr);
      }
      /* Setup Phase Done (control EPs) */
      if ( doepint.b.setup )
      {
        
        /* inform the upper layer that a setup packet is available */
        /* SETUP COMPLETE */
        USBD_DCD_INT_fops->SetupStage(pdev);
        CLEAR_OUT_EP_INTR(epnum, setup);
      }
    }
    epnum++;
    ep_intr >>= 1;
  }
  return 1;
}

/**
* @brief  DCD_HandleSof_ISR
*         Handles the SOF Interrupts
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleSof_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  GINTSTS;
  
  
  USBD_DCD_INT_fops->SOF(pdev);
  
  /* Clear interrupt */
  GINTSTS.d32 = 0;
  GINTSTS.b.sofintr = 1;
  USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GINTSTS, GINTSTS.d32);
  
  return 1;
}

/**
* @brief  DCD_HandleRxStatusQueueLevel_ISR
*         Handles the Rx Status Queue Level Interrupt
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleRxStatusQueueLevel_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTMSK_TypeDef  int_mask;
  USB_OTG_DRXSTS_TypeDef   status;
  USB_OTG_EP *ep;
  
  /* Disable the Rx Status Queue Level interrupt */
  int_mask.d32 = 0;
  int_mask.b.rxstsqlvl = 1;
  USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GINTMSK, int_mask.d32, 0);
  
  /* Get the Status from the top of the FIFO */
  status.d32 = USB_OTG_READ_REG32( &pdev->regs.GREGS->GRXSTSP );
  
  ep = &pdev->dev.out_ep[status.b.epnum];
  
  switch (status.b.pktsts)
  {
  case STS_GOUT_NAK:
    break;
  case STS_DATA_UPDT:
    if (status.b.bcnt)
    {
      USB_OTG_ReadPacket(pdev,ep->xfer_buff, status.b.bcnt);
      ep->xfer_buff += status.b.bcnt;
      ep->xfer_count += status.b.bcnt;
    }
    break;
  case STS_XFER_COMP:
    break;
  case STS_SETUP_COMP:
    break;
  case STS_SETUP_UPDT:
    /* Copy the setup packet received in FIFO into the setup buffer in RAM */
    USB_OTG_ReadPacket(pdev , pdev->dev.setup_packet, 8);
    ep->xfer_count += status.b.bcnt;
    break;
  default:
    break;
  }
  
  /* Enable the Rx Status Queue Level interrupt */
  USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GINTMSK, 0, int_mask.d32);
  
  return 1;
}

/**
* @brief  DCD_WriteEmptyTxFifo
*         check FIFO for the next packet to be loaded
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_WriteEmptyTxFifo(USB_OTG_CORE_HANDLE *pdev, uint32_t epnum)
{
  USB_OTG_DTXFSTSn_TypeDef  txstatus;
  USB_OTG_EP *ep;
  uint32_t len = 0;
  uint32_t len32b;
  txstatus.d32 = 0;
  
  ep = &pdev->dev.in_ep[epnum];    
  
  len = ep->xfer_len - ep->xfer_count;
  
  if (len > ep->maxpacket)
  {
    len = ep->maxpacket;
  }
  
  len32b = (len + 3) / 4;
  txstatus.d32 = USB_OTG_READ_REG32( &pdev->regs.INEP_REGS[epnum]->DTXFSTS);
  
  
  
  while  (txstatus.b.txfspcavail > len32b &&
          ep->xfer_count < ep->xfer_len &&
            ep->xfer_len != 0)
  {
    /* Write the FIFO */
    len = ep->xfer_len - ep->xfer_count;
    
    if (len > ep->maxpacket)
    {
      len = ep->maxpacket;
    }
    len32b = (len + 3) / 4;
    
    USB_OTG_WritePacket (pdev , ep->xfer_buff, epnum, len);
    
    ep->xfer_buff  += len;
    ep->xfer_count += len;
    
    txstatus.d32 = USB_OTG_READ_REG32(&pdev->regs.INEP_REGS[epnum]->DTXFSTS);
  }
  
  return 1;
}

/**
* @brief  DCD_HandleUsbReset_ISR
*         This interrupt occurs when a USB Reset is detected
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleUsbReset_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_DAINT_TypeDef    daintmsk;
  USB_OTG_DOEPMSK_TypeDef  doepmsk;
  USB_OTG_DIEPMSK_TypeDef  diepmsk;
  USB_OTG_DCFG_TypeDef     dcfg;
  USB_OTG_DCTL_TypeDef     dctl;
  USB_OTG_GINTSTS_TypeDef  gintsts;
  uint32_t i;
  
  dctl.d32 = 0;
  daintmsk.d32 = 0;
  doepmsk.d32 = 0;
  diepmsk.d32 = 0;
  dcfg.d32 = 0;
  gintsts.d32 = 0;
  
  /* Clear the Remote Wake-up Signaling */
  dctl.b.rmtwkupsig = 1;
  USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DCTL, dctl.d32, 0 );
  
  /* Flush the Tx FIFO */
  USB_OTG_FlushTxFifo(pdev ,  0 );
  
  for (i = 0; i < pdev->cfg.dev_endpoints ; i++)
  {
    USB_OTG_WRITE_REG32( &pdev->regs.INEP_REGS[i]->DIEPINT, 0xFF);
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[i]->DOEPINT, 0xFF);
  }
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINT, 0xFFFFFFFF );
  
  daintmsk.ep.in = 1;
  daintmsk.ep.out = 1;
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINTMSK, daintmsk.d32 );
  
  doepmsk.b.setup = 1;
  doepmsk.b.xfercompl = 1;
  doepmsk.b.ahberr = 1;
  doepmsk.b.epdisabled = 1;
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DOEPMSK, doepmsk.d32 );
#ifdef USB_OTG_HS_DEDICATED_EP1_ENABLED   
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DOUTEP1MSK, doepmsk.d32 );
#endif
  diepmsk.b.xfercompl = 1;
  diepmsk.b.timeout = 1;
  diepmsk.b.epdisabled = 1;
  diepmsk.b.ahberr = 1;
  diepmsk.b.intknepmis = 1;
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DIEPMSK, diepmsk.d32 );
#ifdef USB_OTG_HS_DEDICATED_EP1_ENABLED  
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DINEP1MSK, diepmsk.d32 );
#endif
  /* Reset Device Address */
  dcfg.d32 = USB_OTG_READ_REG32( &pdev->regs.DREGS->DCFG);
  dcfg.b.devaddr = 0;
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DCFG, dcfg.d32);
  
  
  /* setup EP0 to receive SETUP packets */
  USB_OTG_EP0_OutStart(pdev);
  
  /* Clear interrupt */
  gintsts.d32 = 0;
  gintsts.b.usbreset = 1;
  USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GINTSTS, gintsts.d32);
  
  /*Reset internal state machine */
  USBD_DCD_INT_fops->Reset(pdev);
  return 1;
}

/**
* @brief  DCD_HandleEnumDone_ISR
*         Read the device status register and set the device speed
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_HandleEnumDone_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef  gintsts;
  USB_OTG_GUSBCFG_TypeDef  gusbcfg;
  
  USB_OTG_EP0Activate(pdev);
  
  /* Set USB turn-around time based on device speed and PHY interface. */
  gusbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);
  
  /* Full or High speed */
  if ( USB_OTG_GetDeviceSpeed(pdev) == USB_SPEED_HIGH)
  {
    pdev->cfg.speed            = USB_OTG_SPEED_HIGH;
    pdev->cfg.mps              = USB_OTG_HS_MAX_PACKET_SIZE ;    
    gusbcfg.b.usbtrdtim = 9;
  }
  else
  {
    pdev->cfg.speed            = USB_OTG_SPEED_FULL;
    pdev->cfg.mps              = USB_OTG_FS_MAX_PACKET_SIZE ;  
    gusbcfg.b.usbtrdtim = 5;
  }
  
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GUSBCFG, gusbcfg.d32);
  
  /* Clear interrupt */
  gintsts.d32 = 0;
  gintsts.b.enumdone = 1;
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GINTSTS, gintsts.d32 );
  return 1;
}


/**
* @brief  DCD_IsoINIncomplete_ISR
*         handle the ISO IN incomplete interrupt
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_IsoINIncomplete_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef gintsts;  
  
  gintsts.d32 = 0;

  USBD_DCD_INT_fops->IsoINIncomplete (pdev); 
  
  /* Clear interrupt */
  gintsts.b.incomplisoin = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTSTS, gintsts.d32);
  
  return 1;
}

/**
* @brief  DCD_IsoOUTIncomplete_ISR
*         handle the ISO OUT incomplete interrupt
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_IsoOUTIncomplete_ISR(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTSTS_TypeDef gintsts;  
  
  gintsts.d32 = 0;

  USBD_DCD_INT_fops->IsoOUTIncomplete (pdev); 
  
  /* Clear interrupt */
  gintsts.b.incomplisoout = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTSTS, gintsts.d32);
  return 1;
}
/**
* @brief  DCD_ReadDevInEP
*         Reads ep flags
* @param  pdev: device instance
* @retval status
*/
static uint32_t DCD_ReadDevInEP (USB_OTG_CORE_HANDLE *pdev, uint8_t epnum)
{
  uint32_t v, msk, emp;
  msk = USB_OTG_READ_REG32(&pdev->regs.DREGS->DIEPMSK);
  emp = USB_OTG_READ_REG32(&pdev->regs.DREGS->DIEPEMPMSK);
  msk |= ((emp >> epnum) & 0x1) << 7;
  v = USB_OTG_READ_REG32(&pdev->regs.INEP_REGS[epnum]->DIEPINT) & msk;
  return v;
}



/**
* @}
*/ 

/**
* @}
*/ 

/**
* @}
*/
#endif

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
