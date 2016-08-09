/**
  ******************************************************************************
  * @file    usb_core.c
  * @author  MCD Application Team
  * @version V2.0.0
  * @date    22-July-2011
  * @brief   USB-OTG Core Layer
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

/* Includes ------------------------------------------------------------------*/
#include "usb_core.h"
#include "usb_bsp.h"


/** @addtogroup USB_OTG_DRIVER
* @{
*/

/** @defgroup USB_CORE 
* @brief This file includes the USB-OTG Core Layer
* @{
*/


/** @defgroup USB_CORE_Private_Defines
* @{
*/ 

/**
* @}
*/ 


/** @defgroup USB_CORE_Private_TypesDefinitions
* @{
*/ 
/**
* @}
*/ 



/** @defgroup USB_CORE_Private_Macros
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_CORE_Private_Variables
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_CORE_Private_FunctionPrototypes
* @{
*/ 
/**
* @}
*/ 


/** @defgroup USB_CORE_Private_Functions
* @{
*/ 

/**
* @brief  USB_OTG_EnableCommonInt
*         Initializes the commmon interrupts, used in both device and modes
* @param  pdev : Selected device
* @retval None
*/
static void USB_OTG_EnableCommonInt(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_GINTMSK_TypeDef  int_mask;
  
  int_mask.d32 = 0;
  /* Clear any pending USB_OTG Interrupts */
#ifndef USE_OTG_MODE
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GOTGINT, 0xFFFFFFFF);
#endif
  /* Clear any pending interrupts */
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GINTSTS, 0xFFFFFFFF);
  /* Enable the interrupts in the INTMSK */
  int_mask.b.wkupintr = 1;
  int_mask.b.usbsuspend = 1; 
  
#ifdef USE_OTG_MODE
  int_mask.b.otgintr = 1;
  int_mask.b.sessreqintr = 1;
  int_mask.b.conidstschng = 1;
#endif
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GINTMSK, int_mask.d32);
}

/**
* @brief  USB_OTG_CoreReset : Soft reset of the core
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
static USB_OTG_STS USB_OTG_CoreReset(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS status = USB_OTG_OK;
  __IO USB_OTG_GRSTCTL_TypeDef  greset;
  uint32_t count = 0;
  
  greset.d32 = 0;
  /* Wait for AHB master IDLE state. */
  do
  {
    USB_OTG_BSP_uDelay(3);
    greset.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GRSTCTL);
    if (++count > 200000)
    {
      return USB_OTG_OK;
    }
  }
  while (greset.b.ahbidle == 0);
  /* Core Soft Reset */
  count = 0;
  greset.b.csftrst = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GRSTCTL, greset.d32 );
  do
  {
    greset.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GRSTCTL);
    if (++count > 200000)
    {
      break;
    }
  }
  while (greset.b.csftrst == 1);
  /* Wait for 3 PHY Clocks*/
  USB_OTG_BSP_uDelay(3);
  return status;
}

/**
* @brief  USB_OTG_WritePacket : Writes a packet into the Tx FIFO associated 
*         with the EP
* @param  pdev : Selected device
* @param  src : source pointer
* @param  ch_ep_num : end point number
* @param  bytes : No. of bytes
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_WritePacket(USB_OTG_CORE_HANDLE *pdev, 
                                uint8_t             *src, 
                                uint8_t             ch_ep_num, 
                                uint16_t            len)
{
  USB_OTG_STS status = USB_OTG_OK;
  if (pdev->cfg.dma_enable == 0)
  {
    uint32_t count32b= 0 , i= 0;
    __IO uint32_t *fifo;
    
    count32b =  (len + 3) / 4;
    fifo = pdev->regs.DFIFO[ch_ep_num];
    for (i = 0; i < count32b; i++, src+=4)
    {
      USB_OTG_WRITE_REG32( fifo, *((__packed uint32_t *)src) );
    }
  }
  return status;
}


/**
* @brief  USB_OTG_ReadPacket : Reads a packet from the Rx FIFO
* @param  pdev : Selected device
* @param  dest : Destination Pointer
* @param  bytes : No. of bytes
* @retval None
*/
void *USB_OTG_ReadPacket(USB_OTG_CORE_HANDLE *pdev, 
                         uint8_t *dest, 
                         uint16_t len)
{
  uint32_t i=0;
  uint32_t count32b = (len + 3) / 4;
  
  __IO uint32_t *fifo = pdev->regs.DFIFO[0];
  
  for ( i = 0; i < count32b; i++, dest += 4 )
  {
    *(__packed uint32_t *)dest = USB_OTG_READ_REG32(fifo);
    
  }
  return ((void *)dest);
}

/**
* @brief  USB_OTG_SelectCore 
*         Initialize core registers address.
* @param  pdev : Selected device
* @param  coreID : USB OTG Core ID
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_SelectCore(USB_OTG_CORE_HANDLE *pdev, 
                               USB_OTG_CORE_ID_TypeDef coreID)
{
  uint32_t i , baseAddress = 0;
  USB_OTG_STS status = USB_OTG_OK;
  
  pdev->cfg.dma_enable       = 0;
  
  /* at startup the core is in FS mode */
  pdev->cfg.speed            = USB_OTG_SPEED_FULL;
  pdev->cfg.mps              = USB_OTG_FS_MAX_PACKET_SIZE ;    
    
  /* initialize device cfg following its address */
  if (coreID == USB_OTG_FS_CORE_ID)
  {
    baseAddress                = USB_OTG_FS_BASE_ADDR;
    pdev->cfg.coreID           = USB_OTG_FS_CORE_ID;
    pdev->cfg.host_channels    = 8 ;
    pdev->cfg.dev_endpoints    = 4 ;
    pdev->cfg.TotalFifoSize    = 320; /* in 32-bits */
    pdev->cfg.phy_itface       = USB_OTG_EMBEDDED_PHY;     
    
#ifdef USB_OTG_FS_SOF_OUTPUT_ENABLED    
    pdev->cfg.Sof_output       = 1;    
#endif 
    
#ifdef USB_OTG_FS_LOW_PWR_MGMT_SUPPORT    
    pdev->cfg.low_power        = 1;    
#endif     
  }
  else if (coreID == USB_OTG_HS_CORE_ID)
  {
    baseAddress                = USB_OTG_HS_BASE_ADDR;
    pdev->cfg.coreID           = USB_OTG_HS_CORE_ID;    
    pdev->cfg.host_channels    = 12 ;
    pdev->cfg.dev_endpoints    = 6 ;
    pdev->cfg.TotalFifoSize    = 1280;/* in 32-bits */
    
#ifdef USB_OTG_ULPI_PHY_ENABLED
    pdev->cfg.phy_itface       = USB_OTG_ULPI_PHY;
#else    
 #ifdef USB_OTG_EMBEDDED_PHY_ENABLED
    pdev->cfg.phy_itface       = USB_OTG_EMBEDDED_PHY;
 #else   
   #ifdef USB_OTG_I2C_PHY_ENABLED    
    pdev->cfg.phy_itface       = USB_OTG_I2C_PHY; 
   #endif
 #endif  
#endif      
    
#ifdef USB_OTG_HS_INTERNAL_DMA_ENABLED    
    pdev->cfg.dma_enable       = 1;    
#endif
    
#ifdef USB_OTG_HS_SOF_OUTPUT_ENABLED    
    pdev->cfg.Sof_output       = 1;    
#endif 
    
#ifdef USB_OTG_HS_LOW_PWR_MGMT_SUPPORT    
    pdev->cfg.low_power        = 1;    
#endif 
    
  }
  
  pdev->regs.GREGS = (USB_OTG_GREGS *)(baseAddress + \
    USB_OTG_CORE_GLOBAL_REGS_OFFSET);
  pdev->regs.DREGS =  (USB_OTG_DREGS  *)  (baseAddress + \
    USB_OTG_DEV_GLOBAL_REG_OFFSET);
  
  for (i = 0; i < pdev->cfg.dev_endpoints; i++)
  {
    pdev->regs.INEP_REGS[i]  = (USB_OTG_INEPREGS *)  \
      (baseAddress + USB_OTG_DEV_IN_EP_REG_OFFSET + \
        (i * USB_OTG_EP_REG_OFFSET));
    pdev->regs.OUTEP_REGS[i] = (USB_OTG_OUTEPREGS *) \
      (baseAddress + USB_OTG_DEV_OUT_EP_REG_OFFSET + \
        (i * USB_OTG_EP_REG_OFFSET));
  }
  pdev->regs.HREGS = (USB_OTG_HREGS *)(baseAddress + \
    USB_OTG_HOST_GLOBAL_REG_OFFSET);
  pdev->regs.HPRT0 = (uint32_t *)(baseAddress + USB_OTG_HOST_PORT_REGS_OFFSET);
  
  for (i = 0; i < pdev->cfg.host_channels; i++)
  {
    pdev->regs.HC_REGS[i] = (USB_OTG_HC_REGS *)(baseAddress + \
      USB_OTG_HOST_CHAN_REGS_OFFSET + \
        (i * USB_OTG_CHAN_REGS_OFFSET));
  }
  for (i = 0; i < pdev->cfg.host_channels; i++)
  {
    pdev->regs.DFIFO[i] = (uint32_t *)(baseAddress + USB_OTG_DATA_FIFO_OFFSET +\
      (i * USB_OTG_DATA_FIFO_SIZE));
  }
  pdev->regs.PCGCCTL = (uint32_t *)(baseAddress + USB_OTG_PCGCCTL_OFFSET);
  
  return status;
}


/**
* @brief  USB_OTG_CoreInit
*         Initializes the USB_OTG controller registers and prepares the core
*         device mode or host mode operation.
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_CoreInit(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_GUSBCFG_TypeDef  usbcfg;
  USB_OTG_GCCFG_TypeDef    gccfg;
  USB_OTG_GI2CCTL_TypeDef  i2cctl;
  USB_OTG_GAHBCFG_TypeDef  ahbcfg;
  
  usbcfg.d32 = 0;
  gccfg.d32 = 0;
  ahbcfg.d32 = 0;
  

  
  if (pdev->cfg.phy_itface == USB_OTG_ULPI_PHY)
  {
    gccfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GCCFG);
    gccfg.b.pwdn = 0;
    
    if (pdev->cfg.Sof_output)
    {
      gccfg.b.sofouten = 1;   
    }
    USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GCCFG, gccfg.d32);
    
    /* Init The ULPI Interface */
    usbcfg.d32 = 0;
    usbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);
    
    usbcfg.b.physel            = 0; /* HS Interface */
#ifdef USB_OTG_INTERNAL_VBUS_ENABLED
    usbcfg.b.ulpi_ext_vbus_drv = 0; /* Use internal VBUS */
#else
 #ifdef USB_OTG_EXTERNAL_VBUS_ENABLED    
    usbcfg.b.ulpi_ext_vbus_drv = 1; /* Use external VBUS */
 #endif
#endif 
    usbcfg.b.term_sel_dl_pulse = 0; /* Data line pulsing using utmi_txvalid */    
    usbcfg.b.ulpi_utmi_sel     = 1; /* ULPI seleInterfacect */
    
    usbcfg.b.phyif             = 0; /* 8 bits */
    usbcfg.b.ddrsel            = 0; /* single data rate */
    
    usbcfg.b.ulpi_fsls = 0;
    usbcfg.b.ulpi_clk_sus_m = 0;
    USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GUSBCFG, usbcfg.d32);
    
    /* Reset after a PHY select  */
    USB_OTG_CoreReset(pdev);
    
    if(pdev->cfg.dma_enable == 1)
    {
      
      ahbcfg.b.hburstlen = 5; /* 64 x 32-bits*/
      ahbcfg.b.dmaenable = 1;
      USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GAHBCFG, ahbcfg.d32);
      
    }    
  }
  else /* FS interface (embedded Phy or I2C Phy) */
  {
    
    usbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);;
    usbcfg.b.physel  = 1; /* FS Interface */
    USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GUSBCFG, usbcfg.d32);
    /* Reset after a PHY select and set Host mode */
    USB_OTG_CoreReset(pdev);
    /* Enable the I2C interface and deactivate the power down*/
    gccfg.d32 = 0;
    gccfg.b.pwdn = 1;
    
    if(pdev->cfg.phy_itface == USB_OTG_I2C_PHY)
    {
      gccfg.b.i2cifen = 1;
    }   
    gccfg.b.vbussensingA = 1 ;
    gccfg.b.vbussensingB = 1 ;     
#ifndef VBUS_SENSING_ENABLED
    gccfg.b.disablevbussensing = 1; 
#endif    
    
    if(pdev->cfg.Sof_output)
    {
      gccfg.b.sofouten = 1;  
    }
    
    USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GCCFG, gccfg.d32);
    USB_OTG_BSP_mDelay(20);
    /* Program GUSBCFG.OtgUtmifsSel to I2C*/
    usbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);
    
    if(pdev->cfg.phy_itface == USB_OTG_I2C_PHY)
    {
      usbcfg.b.otgutmifssel = 1;
    }
    
    USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GUSBCFG, usbcfg.d32);
    
    if(pdev->cfg.phy_itface == USB_OTG_I2C_PHY)
    {
      /*Program GI2CCTL.I2CEn*/
      i2cctl.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GI2CCTL);
      i2cctl.b.i2cdevaddr = 1;
      i2cctl.b.i2cen = 0;
      i2cctl.b.dat_se0 = 1;
      i2cctl.b.addr = 0x2D;
      USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GI2CCTL, i2cctl.d32);
      
      USB_OTG_BSP_mDelay(200);
      
      i2cctl.b.i2cen = 1;
      USB_OTG_WRITE_REG32 (&pdev->regs.GREGS->GI2CCTL, i2cctl.d32);
      USB_OTG_BSP_mDelay(200);
    }
  }
  /* case the HS core is working in FS mode */
  if(pdev->cfg.dma_enable == 1)
  {
    
    ahbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GAHBCFG);
    ahbcfg.b.hburstlen = 5; /* 64 x 32-bits*/
    ahbcfg.b.dmaenable = 1;
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GAHBCFG, ahbcfg.d32);
    
  }
  /* initialize OTG features */
#ifdef  USE_OTG_MODE
  usbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);
  usbcfg.b.hnpcap = 1;
  usbcfg.b.srpcap = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GUSBCFG, usbcfg.d32);
  USB_OTG_EnableCommonInt(pdev);
#endif
  return status;
}
/**
* @brief  USB_OTG_EnableGlobalInt
*         Enables the controller's Global Int in the AHB Config reg
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EnableGlobalInt(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_GAHBCFG_TypeDef  ahbcfg;
  
  ahbcfg.d32 = 0;
  ahbcfg.b.glblintrmsk = 1; /* Enable interrupts */
  USB_OTG_MODIFY_REG32(&pdev->regs.GREGS->GAHBCFG, 0, ahbcfg.d32);
  return status;
}


/**
* @brief  USB_OTG_DisableGlobalInt
*         Enables the controller's Global Int in the AHB Config reg
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_DisableGlobalInt(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_GAHBCFG_TypeDef  ahbcfg;
  ahbcfg.d32 = 0;
  ahbcfg.b.glblintrmsk = 1; /* Enable interrupts */
  USB_OTG_MODIFY_REG32(&pdev->regs.GREGS->GAHBCFG, ahbcfg.d32, 0);
  return status;
}


/**
* @brief  USB_OTG_FlushTxFifo : Flush a Tx FIFO
* @param  pdev : Selected device
* @param  num : FO num
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_FlushTxFifo (USB_OTG_CORE_HANDLE *pdev , uint32_t num )
{
  USB_OTG_STS status = USB_OTG_OK;
  __IO USB_OTG_GRSTCTL_TypeDef  greset;
  
  uint32_t count = 0;
  greset.d32 = 0;
  greset.b.txfflsh = 1;
  greset.b.txfnum  = num;
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GRSTCTL, greset.d32 );
  do
  {
    greset.d32 = USB_OTG_READ_REG32( &pdev->regs.GREGS->GRSTCTL);
    if (++count > 200000)
    {
      break;
    }
  }
  while (greset.b.txfflsh == 1);
  /* Wait for 3 PHY Clocks*/
  USB_OTG_BSP_uDelay(3);
  return status;
}


/**
* @brief  USB_OTG_FlushRxFifo : Flush a Rx FIFO
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_FlushRxFifo( USB_OTG_CORE_HANDLE *pdev )
{
  USB_OTG_STS status = USB_OTG_OK;
  __IO USB_OTG_GRSTCTL_TypeDef  greset;
  uint32_t count = 0;
  
  greset.d32 = 0;
  greset.b.rxfflsh = 1;
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GRSTCTL, greset.d32 );
  do
  {
    greset.d32 = USB_OTG_READ_REG32( &pdev->regs.GREGS->GRSTCTL);
    if (++count > 200000)
    {
      break;
    }
  }
  while (greset.b.rxfflsh == 1);
  /* Wait for 3 PHY Clocks*/
  USB_OTG_BSP_uDelay(3);
  return status;
}


/**
* @brief  USB_OTG_SetCurrentMode : Set ID line
* @param  pdev : Selected device
* @param  mode :  (Host/device)
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_SetCurrentMode(USB_OTG_CORE_HANDLE *pdev , uint8_t mode)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_GUSBCFG_TypeDef  usbcfg;
  
  usbcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->GUSBCFG);
  
  usbcfg.b.force_host = 0;
  usbcfg.b.force_dev = 0;
  
  if ( mode == HOST_MODE)
  {
    usbcfg.b.force_host = 1;
  }
  else if ( mode == DEVICE_MODE)
  {
    usbcfg.b.force_dev = 1;
  }
  
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GUSBCFG, usbcfg.d32);
  USB_OTG_BSP_mDelay(50);
  return status;
}


/**
* @brief  USB_OTG_GetMode : Get current mode
* @param  pdev : Selected device
* @retval current mode
*/
uint32_t USB_OTG_GetMode(USB_OTG_CORE_HANDLE *pdev)
{
  return (USB_OTG_READ_REG32(&pdev->regs.GREGS->GINTSTS ) & 0x1);
}


/**
* @brief  USB_OTG_IsDeviceMode : Check if it is device mode
* @param  pdev : Selected device
* @retval num_in_ep
*/
uint8_t USB_OTG_IsDeviceMode(USB_OTG_CORE_HANDLE *pdev)
{
  return (USB_OTG_GetMode(pdev) != HOST_MODE);
}


/**
* @brief  USB_OTG_IsHostMode : Check if it is host mode
* @param  pdev : Selected device
* @retval num_in_ep
*/
uint8_t USB_OTG_IsHostMode(USB_OTG_CORE_HANDLE *pdev)
{
  return (USB_OTG_GetMode(pdev) == HOST_MODE);
}


/**
* @brief  USB_OTG_ReadCoreItr : returns the Core Interrupt register
* @param  pdev : Selected device
* @retval Status
*/
uint32_t USB_OTG_ReadCoreItr(USB_OTG_CORE_HANDLE *pdev)
{
  uint32_t v = 0;
  v = USB_OTG_READ_REG32(&pdev->regs.GREGS->GINTSTS);
  v &= USB_OTG_READ_REG32(&pdev->regs.GREGS->GINTMSK);
  return v;
}


/**
* @brief  USB_OTG_ReadOtgItr : returns the USB_OTG Interrupt register
* @param  pdev : Selected device
* @retval Status
*/
uint32_t USB_OTG_ReadOtgItr (USB_OTG_CORE_HANDLE *pdev)
{
  return (USB_OTG_READ_REG32 (&pdev->regs.GREGS->GOTGINT));
}

#ifdef USE_HOST_MODE
/**
* @brief  USB_OTG_CoreInitHost : Initializes USB_OTG controller for host mode
* @param  pdev : Selected device
* @retval status
*/
USB_OTG_STS USB_OTG_CoreInitHost(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS                     status = USB_OTG_OK;
  USB_OTG_FSIZ_TypeDef            nptxfifosize;
  USB_OTG_FSIZ_TypeDef            ptxfifosize;  
  USB_OTG_HCFG_TypeDef            hcfg;
  
#ifdef USE_OTG_MODE
  USB_OTG_OTGCTL_TypeDef          gotgctl;
#endif
  
  uint32_t                        i = 0;
  
  nptxfifosize.d32 = 0;  
  ptxfifosize.d32 = 0;
#ifdef USE_OTG_MODE
  gotgctl.d32 = 0;
#endif
  hcfg.d32 = 0;
  
  
  /* configure charge pump IO */
  USB_OTG_BSP_ConfigVBUS(pdev);
  
  /* Restart the Phy Clock */
  USB_OTG_WRITE_REG32(pdev->regs.PCGCCTL, 0);
  
  /* Initialize Host Configuration Register */
  USB_OTG_InitFSLSPClkSel(pdev , HCFG_48_MHZ); /* in init phase */
  
  hcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.HREGS->HCFG);
  hcfg.b.fslssupp = 0;
  USB_OTG_WRITE_REG32(&pdev->regs.HREGS->HCFG, hcfg.d32);
  
  /* Configure data FIFO sizes */
  /* Rx FIFO */
#ifdef USB_OTG_FS_CORE
  if(pdev->cfg.coreID == USB_OTG_FS_CORE_ID)
  {
    /* set Rx FIFO size */
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GRXFSIZ, RX_FIFO_FS_SIZE);
    nptxfifosize.b.startaddr = RX_FIFO_FS_SIZE;   
    nptxfifosize.b.depth = TXH_NP_FS_FIFOSIZ;  
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->DIEPTXF0_HNPTXFSIZ, nptxfifosize.d32);
    
    ptxfifosize.b.startaddr = RX_FIFO_FS_SIZE + TXH_NP_FS_FIFOSIZ;
    ptxfifosize.b.depth     = TXH_P_FS_FIFOSIZ;
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->HPTXFSIZ, ptxfifosize.d32);      
  }
#endif
#ifdef USB_OTG_HS_CORE  
   if (pdev->cfg.coreID == USB_OTG_HS_CORE_ID)
  {
   /* set Rx FIFO size */
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GRXFSIZ, RX_FIFO_HS_SIZE);
    nptxfifosize.b.startaddr = RX_FIFO_HS_SIZE;   
    nptxfifosize.b.depth = TXH_NP_HS_FIFOSIZ;  
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->DIEPTXF0_HNPTXFSIZ, nptxfifosize.d32);
    
    ptxfifosize.b.startaddr = RX_FIFO_HS_SIZE + TXH_NP_HS_FIFOSIZ;
    ptxfifosize.b.depth     = TXH_P_HS_FIFOSIZ;
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->HPTXFSIZ, ptxfifosize.d32);      
  }
#endif  
  
#ifdef USE_OTG_MODE
  /* Clear Host Set HNP Enable in the USB_OTG Control Register */
  gotgctl.b.hstsethnpen = 1;
  USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GOTGCTL, gotgctl.d32, 0);
#endif
  
  /* Make sure the FIFOs are flushed. */
  USB_OTG_FlushTxFifo(pdev, 0x10 );         /* all Tx FIFOs */
  USB_OTG_FlushRxFifo(pdev);
  
  
  /* Clear all pending HC Interrupts */
  for (i = 0; i < pdev->cfg.host_channels; i++)
  {
    USB_OTG_WRITE_REG32( &pdev->regs.HC_REGS[i]->HCINT, 0xFFFFFFFF );
    USB_OTG_WRITE_REG32( &pdev->regs.HC_REGS[i]->HCGINTMSK, 0 );
  }
#ifndef USE_OTG_MODE
  USB_OTG_DriveVbus(pdev, 1);
#endif
  
  USB_OTG_EnableHostInt(pdev);
  return status;
}

/**
* @brief  USB_OTG_IsEvenFrame 
*         This function returns the frame number for sof packet
* @param  pdev : Selected device
* @retval Frame number
*/
uint8_t USB_OTG_IsEvenFrame (USB_OTG_CORE_HANDLE *pdev) 
{
  return !(USB_OTG_READ_REG32(&pdev->regs.HREGS->HFNUM) & 0x1);
}

/**
* @brief  USB_OTG_DriveVbus : set/reset vbus
* @param  pdev : Selected device
* @param  state : VBUS state
* @retval None
*/
void USB_OTG_DriveVbus (USB_OTG_CORE_HANDLE *pdev, uint8_t state)
{
  USB_OTG_HPRT0_TypeDef     hprt0;
  
  hprt0.d32 = 0;
  
  /* enable disable the external charge pump */
  USB_OTG_BSP_DriveVBUS(pdev, state);
  
  /* Turn on the Host port power. */
  hprt0.d32 = USB_OTG_ReadHPRT0(pdev);
  if ((hprt0.b.prtpwr == 0 ) && (state == 1 ))
  {
    hprt0.b.prtpwr = 1;
    USB_OTG_WRITE_REG32(pdev->regs.HPRT0, hprt0.d32);
  }
  if ((hprt0.b.prtpwr == 1 ) && (state == 0 ))
  {
    hprt0.b.prtpwr = 0;
    USB_OTG_WRITE_REG32(pdev->regs.HPRT0, hprt0.d32);
  }
  
  USB_OTG_BSP_mDelay(200);
}
/**
* @brief  USB_OTG_EnableHostInt: Enables the Host mode interrupts
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EnableHostInt(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS       status = USB_OTG_OK;
  USB_OTG_GINTMSK_TypeDef  intmsk;
  intmsk.d32 = 0;
  /* Disable all interrupts. */
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTMSK, 0);
  
  /* Clear any pending interrupts. */
  USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GINTSTS, 0xFFFFFFFF);
  
  /* Enable the common interrupts */
  USB_OTG_EnableCommonInt(pdev);
  
  if (pdev->cfg.dma_enable == 0)
  {  
    intmsk.b.rxstsqlvl  = 1;
  }  
  intmsk.b.portintr   = 1;
  intmsk.b.hcintr     = 1;
  intmsk.b.disconnect = 1;  
  intmsk.b.sofintr    = 1;  
  intmsk.b.incomplisoout  = 1; 
  USB_OTG_MODIFY_REG32(&pdev->regs.GREGS->GINTMSK, intmsk.d32, intmsk.d32);
  return status;
}

/**
* @brief  USB_OTG_InitFSLSPClkSel : Initializes the FSLSPClkSel field of the 
*         HCFG register on the PHY type
* @param  pdev : Selected device
* @param  freq : clock frequency
* @retval None
*/
void USB_OTG_InitFSLSPClkSel(USB_OTG_CORE_HANDLE *pdev , uint8_t freq)
{
  USB_OTG_HCFG_TypeDef   hcfg;
  
  hcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.HREGS->HCFG);
  hcfg.b.fslspclksel = freq;
  USB_OTG_WRITE_REG32(&pdev->regs.HREGS->HCFG, hcfg.d32);
}


/**
* @brief  USB_OTG_ReadHPRT0 : Reads HPRT0 to modify later
* @param  pdev : Selected device
* @retval HPRT0 value
*/
uint32_t USB_OTG_ReadHPRT0(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_HPRT0_TypeDef  hprt0;
  
  hprt0.d32 = USB_OTG_READ_REG32(pdev->regs.HPRT0);
  hprt0.b.prtena = 0;
  hprt0.b.prtconndet = 0;
  hprt0.b.prtenchng = 0;
  hprt0.b.prtovrcurrchng = 0;
  return hprt0.d32;
}


/**
* @brief  USB_OTG_ReadHostAllChannels_intr : Register PCD Callbacks
* @param  pdev : Selected device
* @retval Status
*/
uint32_t USB_OTG_ReadHostAllChannels_intr (USB_OTG_CORE_HANDLE *pdev)
{
  return (USB_OTG_READ_REG32 (&pdev->regs.HREGS->HAINT));
}


/**
* @brief  USB_OTG_ResetPort : Reset Host Port
* @param  pdev : Selected device
* @retval status
* @note : (1)The application must wait at least 10 ms (+ 10 ms security)
*   before clearing the reset bit.
*/
uint32_t USB_OTG_ResetPort(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_HPRT0_TypeDef  hprt0;
  
  hprt0.d32 = USB_OTG_ReadHPRT0(pdev);
  hprt0.b.prtrst = 1;
  USB_OTG_WRITE_REG32(pdev->regs.HPRT0, hprt0.d32);
  USB_OTG_BSP_mDelay (10);                                /* See Note #1 */
  hprt0.b.prtrst = 0;
  USB_OTG_WRITE_REG32(pdev->regs.HPRT0, hprt0.d32);
  USB_OTG_BSP_mDelay (20);   
  return 1;
}


/**
* @brief  USB_OTG_HC_Init : Prepares a host channel for transferring packets
* @param  pdev : Selected device
* @param  hc_num : channel number
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_HC_Init(USB_OTG_CORE_HANDLE *pdev , uint8_t hc_num)
{
  USB_OTG_STS status = USB_OTG_OK;
  uint32_t intr_enable = 0;
  USB_OTG_HCGINTMSK_TypeDef  hcintmsk;
  USB_OTG_GINTMSK_TypeDef    gintmsk;
  USB_OTG_HCCHAR_TypeDef     hcchar;
  USB_OTG_HCINTn_TypeDef     hcint;
  
  
  gintmsk.d32 = 0;
  hcintmsk.d32 = 0;
  hcchar.d32 = 0;
  
  /* Clear old interrupt conditions for this host channel. */
  hcint.d32 = 0xFFFFFFFF;
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCINT, hcint.d32);
  
  /* Enable channel interrupts required for this transfer. */
  hcintmsk.d32 = 0;
  
  if (pdev->cfg.dma_enable == 1)
  {
    hcintmsk.b.ahberr = 1;
  }
  
  switch (pdev->host.hc[hc_num].ep_type) 
  {
  case EP_TYPE_CTRL:
  case EP_TYPE_BULK:
    hcintmsk.b.xfercompl = 1;
    hcintmsk.b.stall = 1;
    hcintmsk.b.xacterr = 1;
    hcintmsk.b.datatglerr = 1;
    hcintmsk.b.nak = 1;  
    if (pdev->host.hc[hc_num].ep_is_in) 
    {
      hcintmsk.b.bblerr = 1;
    } 
    else 
    {
      hcintmsk.b.nyet = 1;
      if (pdev->host.hc[hc_num].do_ping) 
      {
        hcintmsk.b.ack = 1;
      }
    }
    break;
  case EP_TYPE_INTR:
    hcintmsk.b.xfercompl = 1;
    hcintmsk.b.nak = 1;
    hcintmsk.b.stall = 1;
    hcintmsk.b.xacterr = 1;
    hcintmsk.b.datatglerr = 1;
    hcintmsk.b.frmovrun = 1;
    
    if (pdev->host.hc[hc_num].ep_is_in) 
    {
      hcintmsk.b.bblerr = 1;
    }
    
    break;
  case EP_TYPE_ISOC:
    hcintmsk.b.xfercompl = 1;
    hcintmsk.b.frmovrun = 1;
    hcintmsk.b.ack = 1;
    
    if (pdev->host.hc[hc_num].ep_is_in) 
    {
      hcintmsk.b.xacterr = 1;
      hcintmsk.b.bblerr = 1;
    }
    break;
  }
  
  
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCGINTMSK, hcintmsk.d32);
  
  
  /* Enable the top level host channel interrupt. */
  intr_enable = (1 << hc_num);
  USB_OTG_MODIFY_REG32(&pdev->regs.HREGS->HAINTMSK, 0, intr_enable);
  
  /* Make sure host channel interrupts are enabled. */
  gintmsk.b.hcintr = 1;
  USB_OTG_MODIFY_REG32(&pdev->regs.GREGS->GINTMSK, 0, gintmsk.d32);
  
  /* Program the HCCHAR register */
  hcchar.d32 = 0;
  hcchar.b.devaddr = pdev->host.hc[hc_num].dev_addr;
  hcchar.b.epnum   = pdev->host.hc[hc_num].ep_num;
  hcchar.b.epdir   = pdev->host.hc[hc_num].ep_is_in;
  hcchar.b.lspddev = (pdev->host.hc[hc_num].speed == HPRT0_PRTSPD_LOW_SPEED);
  hcchar.b.eptype  = pdev->host.hc[hc_num].ep_type;
  hcchar.b.mps     = pdev->host.hc[hc_num].max_packet;
  if (pdev->host.hc[hc_num].ep_type == HCCHAR_INTR)
  {
    hcchar.b.oddfrm  = 1;
  }
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR, hcchar.d32);
  return status;
}


/**
* @brief  USB_OTG_HC_StartXfer : Start transfer
* @param  pdev : Selected device
* @param  hc_num : channel number
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_HC_StartXfer(USB_OTG_CORE_HANDLE *pdev , uint8_t hc_num)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_HCCHAR_TypeDef   hcchar;
  USB_OTG_HCTSIZn_TypeDef  hctsiz;
  USB_OTG_HNPTXSTS_TypeDef hnptxsts; 
  USB_OTG_HPTXSTS_TypeDef  hptxsts; 
  USB_OTG_GINTMSK_TypeDef  intmsk;
  uint16_t                 len_words = 0;   
  
  uint16_t num_packets;
  uint16_t max_hc_pkt_count;
  
  max_hc_pkt_count = 256;
  hctsiz.d32 = 0;
  hcchar.d32 = 0;
  intmsk.d32 = 0;
  
  /* Compute the expected number of packets associated to the transfer */
  if (pdev->host.hc[hc_num].xfer_len > 0)
  {
    num_packets = (pdev->host.hc[hc_num].xfer_len + \
      pdev->host.hc[hc_num].max_packet - 1) / pdev->host.hc[hc_num].max_packet;
    
    if (num_packets > max_hc_pkt_count)
    {
      num_packets = max_hc_pkt_count;
      pdev->host.hc[hc_num].xfer_len = num_packets * \
        pdev->host.hc[hc_num].max_packet;
    }
  }
  else
  {
    num_packets = 1;
  }
  if (pdev->host.hc[hc_num].ep_is_in)
  {
    pdev->host.hc[hc_num].xfer_len = num_packets * \
      pdev->host.hc[hc_num].max_packet;
  }
  /* Initialize the HCTSIZn register */
  hctsiz.b.xfersize = pdev->host.hc[hc_num].xfer_len;
  hctsiz.b.pktcnt = num_packets;
  hctsiz.b.pid = pdev->host.hc[hc_num].data_pid;
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCTSIZ, hctsiz.d32);
  
  if (pdev->cfg.dma_enable == 1)
  {
    USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCDMA, (unsigned int)pdev->host.hc[hc_num].xfer_buff);
  }
  
  
  hcchar.d32 = USB_OTG_READ_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR);
  hcchar.b.oddfrm = USB_OTG_IsEvenFrame(pdev);
  
  /* Set host channel enable */
  hcchar.b.chen = 1;
  hcchar.b.chdis = 0;
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR, hcchar.d32);

  if (pdev->cfg.dma_enable == 0) /* Slave mode */
  {  
    if((pdev->host.hc[hc_num].ep_is_in == 0) && 
        (pdev->host.hc[hc_num].xfer_len > 0))
    {
      switch(pdev->host.hc[hc_num].ep_type) 
      {
        /* Non periodic transfer */
      case EP_TYPE_CTRL:
      case EP_TYPE_BULK:
        
        hnptxsts.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->HNPTXSTS);
        len_words = (pdev->host.hc[hc_num].xfer_len + 3) / 4;
        
        /* check if there is enough space in FIFO space */
        if(len_words > hnptxsts.b.nptxfspcavail)
        {
          /* need to process data in nptxfempty interrupt */
          intmsk.b.nptxfempty = 1;
          USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GINTMSK, 0, intmsk.d32);  
        }
        
        break;
        /* Periodic transfer */
      case EP_TYPE_INTR:
      case EP_TYPE_ISOC:
        hptxsts.d32 = USB_OTG_READ_REG32(&pdev->regs.HREGS->HPTXSTS);
        len_words = (pdev->host.hc[hc_num].xfer_len + 3) / 4;
        /* check if there is enough space in FIFO space */
        if(len_words > hptxsts.b.ptxfspcavail) /* split the transfer */
        {
          /* need to process data in ptxfempty interrupt */
          intmsk.b.ptxfempty = 1;
          USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GINTMSK, 0, intmsk.d32);  
        }
        break;
        
      default:
        break;
      }
      
      /* Write packet into the Tx FIFO. */
      USB_OTG_WritePacket(pdev, 
                          pdev->host.hc[hc_num].xfer_buff , 
                          hc_num, pdev->host.hc[hc_num].xfer_len);
    }
  }
  return status;
}


/**
* @brief  USB_OTG_HC_Halt : Halt channel
* @param  pdev : Selected device
* @param  hc_num : channel number
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_HC_Halt(USB_OTG_CORE_HANDLE *pdev , uint8_t hc_num)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_HNPTXSTS_TypeDef            nptxsts;
  USB_OTG_HPTXSTS_TypeDef             hptxsts;
  USB_OTG_HCCHAR_TypeDef              hcchar;
  
  nptxsts.d32 = 0;
  hptxsts.d32 = 0;
  hcchar.d32 = USB_OTG_READ_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR);
  hcchar.b.chen = 1;
  hcchar.b.chdis = 1;
  
  /* Check for space in the request queue to issue the halt. */
  if (hcchar.b.eptype == HCCHAR_CTRL || hcchar.b.eptype == HCCHAR_BULK)
  {
    nptxsts.d32 = USB_OTG_READ_REG32(&pdev->regs.GREGS->HNPTXSTS);
    if (nptxsts.b.nptxqspcavail == 0)
    {
      hcchar.b.chen = 0;
    }
  }
  else
  {
    hptxsts.d32 = USB_OTG_READ_REG32(&pdev->regs.HREGS->HPTXSTS);
    if (hptxsts.b.ptxqspcavail == 0)
    {
      hcchar.b.chen = 0;
    }
  }
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR, hcchar.d32);
  return status;
}

/**
* @brief  Issue a ping token
* @param  None
* @retval : None
*/
USB_OTG_STS USB_OTG_HC_DoPing(USB_OTG_CORE_HANDLE *pdev , uint8_t hc_num)
{
  USB_OTG_STS               status = USB_OTG_OK;
  USB_OTG_HCCHAR_TypeDef    hcchar;
  USB_OTG_HCTSIZn_TypeDef   hctsiz;  
 
  hctsiz.d32 = 0;
  hctsiz.b.dopng = 1;
  hctsiz.b.pktcnt = 1;
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCTSIZ, hctsiz.d32);
  
  hcchar.d32 = USB_OTG_READ_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR);
  hcchar.b.chen = 1;
  hcchar.b.chdis = 0;
  USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[hc_num]->HCCHAR, hcchar.d32);
  return status;  
}

/**
* @brief  Stop the device and clean up fifo's
* @param  None
* @retval : None
*/
void USB_OTG_StopHost(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_HCCHAR_TypeDef  hcchar;
  uint32_t                i;
  
  USB_OTG_WRITE_REG32(&pdev->regs.HREGS->HAINTMSK , 0);
  USB_OTG_WRITE_REG32(&pdev->regs.HREGS->HAINT,      0xFFFFFFFF);
  /* Flush out any leftover queued requests. */
  
  for (i = 0; i < pdev->cfg.host_channels; i++)
  {
    hcchar.d32 = USB_OTG_READ_REG32(&pdev->regs.HC_REGS[i]->HCCHAR);
    hcchar.b.chen = 0;
    hcchar.b.chdis = 1;
    hcchar.b.epdir = 0;
    USB_OTG_WRITE_REG32(&pdev->regs.HC_REGS[i]->HCCHAR, hcchar.d32);
  }
  
  /* Flush the FIFO */
  USB_OTG_FlushRxFifo(pdev);
  USB_OTG_FlushTxFifo(pdev ,  0x10 );  
}
#endif
#ifdef USE_DEVICE_MODE
/*         PCD Core Layer       */

/**
* @brief  USB_OTG_InitDevSpeed :Initializes the DevSpd field of DCFG register 
*         depending the PHY type and the enumeration speed of the device.
* @param  pdev : Selected device
* @retval : None
*/
void USB_OTG_InitDevSpeed(USB_OTG_CORE_HANDLE *pdev , uint8_t speed)
{
  USB_OTG_DCFG_TypeDef   dcfg;
  
  dcfg.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DCFG);
  dcfg.b.devspd = speed;
  USB_OTG_WRITE_REG32(&pdev->regs.DREGS->DCFG, dcfg.d32);
}


/**
* @brief  USB_OTG_CoreInitDev : Initializes the USB_OTG controller registers 
*         for device mode
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_CoreInitDev (USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS             status       = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef  depctl;
  uint32_t i;
  USB_OTG_DCFG_TypeDef    dcfg;
  USB_OTG_FSIZ_TypeDef    nptxfifosize;
  USB_OTG_FSIZ_TypeDef    txfifosize;
  USB_OTG_DIEPMSK_TypeDef msk;
  USB_OTG_DTHRCTL_TypeDef dthrctl;  
  
  depctl.d32 = 0;
  dcfg.d32 = 0;
  nptxfifosize.d32 = 0;
  txfifosize.d32 = 0;
  msk.d32 = 0;
  
  /* Restart the Phy Clock */
  USB_OTG_WRITE_REG32(pdev->regs.PCGCCTL, 0);
  /* Device configuration register */
  dcfg.d32 = USB_OTG_READ_REG32( &pdev->regs.DREGS->DCFG);
  dcfg.b.perfrint = DCFG_FRAME_INTERVAL_80;
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DCFG, dcfg.d32 );
  
#ifdef USB_OTG_FS_CORE
  if(pdev->cfg.coreID == USB_OTG_FS_CORE_ID  )
  {  
    
    /* Set Full speed phy */
    USB_OTG_InitDevSpeed (pdev , USB_OTG_SPEED_PARAM_FULL);
    
    /* set Rx FIFO size */
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GRXFSIZ, RX_FIFO_FS_SIZE);
    
    /* EP0 TX*/
    nptxfifosize.b.depth     = TX0_FIFO_FS_SIZE;
    nptxfifosize.b.startaddr = RX_FIFO_FS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF0_HNPTXFSIZ, nptxfifosize.d32 );
    
    
    /* EP1 TX*/
    txfifosize.b.startaddr = nptxfifosize.b.startaddr + nptxfifosize.b.depth;
    txfifosize.b.depth = TX1_FIFO_FS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[0], txfifosize.d32 );
    
    
    /* EP2 TX*/
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX2_FIFO_FS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[1], txfifosize.d32 );
    
    
    /* EP3 TX*/  
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX3_FIFO_FS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[2], txfifosize.d32 );
  }
#endif
#ifdef USB_OTG_HS_CORE
  if(pdev->cfg.coreID == USB_OTG_HS_CORE_ID  )
  {
    
    /* Set High speed phy */
    
    if(pdev->cfg.phy_itface  == USB_OTG_ULPI_PHY)
    {
       USB_OTG_InitDevSpeed (pdev , USB_OTG_SPEED_PARAM_HIGH);
    }
    else /* set High speed phy in Full speed mode */
    {
      USB_OTG_InitDevSpeed (pdev , USB_OTG_SPEED_PARAM_HIGH_IN_FULL);
    }
    
    /* set Rx FIFO size */
    USB_OTG_WRITE_REG32(&pdev->regs.GREGS->GRXFSIZ, RX_FIFO_HS_SIZE);
    
    /* EP0 TX*/
    nptxfifosize.b.depth     = TX0_FIFO_HS_SIZE;
    nptxfifosize.b.startaddr = RX_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF0_HNPTXFSIZ, nptxfifosize.d32 );
    
    
    /* EP1 TX*/
    txfifosize.b.startaddr = nptxfifosize.b.startaddr + nptxfifosize.b.depth;
    txfifosize.b.depth = TX1_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[0], txfifosize.d32 );
    
    
    /* EP2 TX*/
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX2_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[1], txfifosize.d32 );
    
    
    /* EP3 TX*/  
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX3_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[2], txfifosize.d32 );
    
    /* EP4 TX*/
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX4_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[3], txfifosize.d32 );
    
    
    /* EP5 TX*/  
    txfifosize.b.startaddr += txfifosize.b.depth;
    txfifosize.b.depth = TX5_FIFO_HS_SIZE;
    USB_OTG_WRITE_REG32( &pdev->regs.GREGS->DIEPTXF[4], txfifosize.d32 );
  }
#endif  
  /* Flush the FIFOs */
  USB_OTG_FlushTxFifo(pdev , 0x10); /* all Tx FIFOs */
  USB_OTG_FlushRxFifo(pdev);
  /* Clear all pending Device Interrupts */
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DIEPMSK, 0 );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DOEPMSK, 0 );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINT, 0xFFFFFFFF );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINTMSK, 0 );
  
  for (i = 0; i < pdev->cfg.dev_endpoints; i++)
  {
    depctl.d32 = USB_OTG_READ_REG32(&pdev->regs.INEP_REGS[i]->DIEPCTL);
    if (depctl.b.epena)
    {
      depctl.d32 = 0;
      depctl.b.epdis = 1;
      depctl.b.snak = 1;
    }
    else
    {
      depctl.d32 = 0;
    }
    USB_OTG_WRITE_REG32( &pdev->regs.INEP_REGS[i]->DIEPCTL, depctl.d32);
    USB_OTG_WRITE_REG32( &pdev->regs.INEP_REGS[i]->DIEPTSIZ, 0);
    USB_OTG_WRITE_REG32( &pdev->regs.INEP_REGS[i]->DIEPINT, 0xFF);
  }
  for (i = 0; i <  pdev->cfg.dev_endpoints; i++)
  {
    USB_OTG_DEPCTL_TypeDef  depctl;
    depctl.d32 = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[i]->DOEPCTL);
    if (depctl.b.epena)
    {
      depctl.d32 = 0;
      depctl.b.epdis = 1;
      depctl.b.snak = 1;
    }
    else
    {
      depctl.d32 = 0;
    }
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[i]->DOEPCTL, depctl.d32);
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[i]->DOEPTSIZ, 0);
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[i]->DOEPINT, 0xFF);
  }
  msk.d32 = 0;
  msk.b.txfifoundrn = 1;
  USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DIEPMSK, msk.d32, msk.d32);
  
  if (pdev->cfg.dma_enable == 1)
  {
    dthrctl.d32 = 0;
    dthrctl.b.non_iso_thr_en = 1;
    dthrctl.b.iso_thr_en = 1;
    dthrctl.b.tx_thr_len = 64;
    dthrctl.b.rx_thr_en = 1;
    dthrctl.b.rx_thr_len = 64;
    USB_OTG_WRITE_REG32(&pdev->regs.DREGS->DTHRCTL, dthrctl.d32);  
  }
  USB_OTG_EnableDevInt(pdev);
  return status;
}


/**
* @brief  USB_OTG_EnableDevInt : Enables the Device mode interrupts
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EnableDevInt(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_GINTMSK_TypeDef  intmsk;
  
  intmsk.d32 = 0;
  
  /* Disable all interrupts. */
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GINTMSK, 0);
  /* Clear any pending interrupts */
  USB_OTG_WRITE_REG32( &pdev->regs.GREGS->GINTSTS, 0xFFFFFFFF);
  /* Enable the common interrupts */
  USB_OTG_EnableCommonInt(pdev);
  
  if (pdev->cfg.dma_enable == 0)
  {
    intmsk.b.rxstsqlvl = 1;
  }
  
  /* Enable interrupts matching to the Device mode ONLY */
  intmsk.b.usbsuspend = 1;
  intmsk.b.usbreset   = 1;
  intmsk.b.enumdone   = 1;
  intmsk.b.inepintr   = 1;
  intmsk.b.outepintr  = 1;
  intmsk.b.sofintr    = 1; 

  intmsk.b.incomplisoin    = 1; 
  intmsk.b.incomplisoout    = 1;   
#ifdef VBUS_SENSING_ENABLED
  intmsk.b.sessreqintr    = 1; 
  intmsk.b.otgintr    = 1;    
#endif  
  USB_OTG_MODIFY_REG32( &pdev->regs.GREGS->GINTMSK, intmsk.d32, intmsk.d32);
  return status;
}


/**
* @brief  USB_OTG_GetDeviceSpeed
*         Get the device speed from the device status register
* @param  None
* @retval status
*/
enum USB_OTG_SPEED USB_OTG_GetDeviceSpeed (USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_DSTS_TypeDef  dsts;
  enum USB_OTG_SPEED speed = USB_SPEED_UNKNOWN;
  
  
  dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
  
  switch (dsts.b.enumspd)
  {
  case DSTS_ENUMSPD_HS_PHY_30MHZ_OR_60MHZ:
    speed = USB_SPEED_HIGH;
    break;
  case DSTS_ENUMSPD_FS_PHY_30MHZ_OR_60MHZ:
  case DSTS_ENUMSPD_FS_PHY_48MHZ:
    speed = USB_SPEED_FULL;
    break;
    
  case DSTS_ENUMSPD_LS_PHY_6MHZ:
    speed = USB_SPEED_LOW;
    break;
  }
  
  return speed;
}
/**
* @brief  enables EP0 OUT to receive SETUP packets and configures EP0
*   for transmitting packets
* @param  None
* @retval USB_OTG_STS : status
*/
USB_OTG_STS  USB_OTG_EP0Activate(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_STS             status = USB_OTG_OK;
  USB_OTG_DSTS_TypeDef    dsts;
  USB_OTG_DEPCTL_TypeDef  diepctl;
  USB_OTG_DCTL_TypeDef    dctl;
  
  dctl.d32 = 0;
  /* Read the Device Status and Endpoint 0 Control registers */
  dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
  diepctl.d32 = USB_OTG_READ_REG32(&pdev->regs.INEP_REGS[0]->DIEPCTL);
  /* Set the MPS of the IN EP based on the enumeration speed */
  switch (dsts.b.enumspd)
  {
  case DSTS_ENUMSPD_HS_PHY_30MHZ_OR_60MHZ:
  case DSTS_ENUMSPD_FS_PHY_30MHZ_OR_60MHZ:
  case DSTS_ENUMSPD_FS_PHY_48MHZ:
    diepctl.b.mps = DEP0CTL_MPS_64;
    break;
  case DSTS_ENUMSPD_LS_PHY_6MHZ:
    diepctl.b.mps = DEP0CTL_MPS_8;
    break;
  }
  USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[0]->DIEPCTL, diepctl.d32);
  dctl.b.cgnpinnak = 1;
  USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DCTL, dctl.d32, dctl.d32);
  return status;
}


/**
* @brief  USB_OTG_EPActivate : Activates an EP
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EPActivate(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef  depctl;
  USB_OTG_DAINT_TypeDef  daintmsk;
  __IO uint32_t *addr;
  
  
  depctl.d32 = 0;
  daintmsk.d32 = 0;
  /* Read DEPCTLn register */
  if (ep->is_in == 1)
  {
    addr = &pdev->regs.INEP_REGS[ep->num]->DIEPCTL;
    daintmsk.ep.in = 1 << ep->num;
  }
  else
  {
    addr = &pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL;
    daintmsk.ep.out = 1 << ep->num;
  }
  /* If the EP is already active don't change the EP Control
  * register. */
  depctl.d32 = USB_OTG_READ_REG32(addr);
  if (!depctl.b.usbactep)
  {
    depctl.b.mps    = ep->maxpacket;
    depctl.b.eptype = ep->type;
    depctl.b.txfnum = ep->tx_fifo_num;
    depctl.b.setd0pid = 1;
    depctl.b.usbactep = 1;
    USB_OTG_WRITE_REG32(addr, depctl.d32);
  }
  /* Enable the Interrupt for this EP */
#ifdef USB_OTG_HS_DEDICATED_EP1_ENABLED
  if((ep->num == 1)&&(pdev->cfg.coreID == USB_OTG_HS_CORE_ID))
  {
    USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DEACHMSK, 0, daintmsk.d32);
  }
  else
#endif   
    USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DAINTMSK, 0, daintmsk.d32);
  return status;
}


/**
* @brief  USB_OTG_EPDeactivate : Deactivates an EP
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EPDeactivate(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef  depctl;
  USB_OTG_DAINT_TypeDef  daintmsk;
  __IO uint32_t *addr;
  
  depctl.d32 = 0;
  daintmsk.d32 = 0;  
  /* Read DEPCTLn register */
  if (ep->is_in == 1)
  {
    addr = &pdev->regs.INEP_REGS[ep->num]->DIEPCTL;
    daintmsk.ep.in = 1 << ep->num;
  }
  else
  {
    addr = &pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL;
    daintmsk.ep.out = 1 << ep->num;
  }
  depctl.b.usbactep = 0;
  USB_OTG_WRITE_REG32(addr, depctl.d32);
  /* Disable the Interrupt for this EP */
  
#ifdef USB_OTG_HS_DEDICATED_EP1_ENABLED
  if((ep->num == 1)&&(pdev->cfg.coreID == USB_OTG_HS_CORE_ID))
  {
    USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DEACHMSK, daintmsk.d32, 0);
  }
  else
#endif    
    USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DAINTMSK, daintmsk.d32, 0);
  return status;
}


/**
* @brief  USB_OTG_EPStartXfer : Handle the setup for data xfer for an EP and 
*         starts the xfer
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EPStartXfer(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef     depctl;
  USB_OTG_DEPXFRSIZ_TypeDef  deptsiz;
  USB_OTG_DSTS_TypeDef       dsts;    
  uint32_t fifoemptymsk = 0;  
  
  depctl.d32 = 0;
  deptsiz.d32 = 0;
  /* IN endpoint */
  if (ep->is_in == 1)
  {
    depctl.d32  = USB_OTG_READ_REG32(&(pdev->regs.INEP_REGS[ep->num]->DIEPCTL));
    deptsiz.d32 = USB_OTG_READ_REG32(&(pdev->regs.INEP_REGS[ep->num]->DIEPTSIZ));
    /* Zero Length Packet? */
    if (ep->xfer_len == 0)
    {
      deptsiz.b.xfersize = 0;
      deptsiz.b.pktcnt = 1;
    }
    else
    {
      /* Program the transfer size and packet count
      * as follows: xfersize = N * maxpacket +
      * short_packet pktcnt = N + (short_packet
      * exist ? 1 : 0)
      */
      deptsiz.b.xfersize = ep->xfer_len;
      deptsiz.b.pktcnt = (ep->xfer_len - 1 + ep->maxpacket) / ep->maxpacket;

      if (ep->type == EP_TYPE_ISOC)
      {
        deptsiz.b.mc = 1;
      }       
    }
    USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[ep->num]->DIEPTSIZ, deptsiz.d32);
    
    if (pdev->cfg.dma_enable == 1)
    {
      USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[ep->num]->DIEPDMA, ep->dma_addr);
    }
    else
    {
      if (ep->type != EP_TYPE_ISOC)
      {
        /* Enable the Tx FIFO Empty Interrupt for this EP */
        if (ep->xfer_len > 0)
        {
          fifoemptymsk = 1 << ep->num;
          USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DIEPEMPMSK, 0, fifoemptymsk);
        }
      }
    }
    
    
    if (ep->type == EP_TYPE_ISOC)
    {
      dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
      
      if (((dsts.b.soffn)&0x1) == 0)
      {
        depctl.b.setd1pid = 1;
      }
      else
      {
        depctl.b.setd0pid = 1;
      }
    } 
    
    /* EP enable, IN data in FIFO */
    depctl.b.cnak = 1;
    depctl.b.epena = 1;
    USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[ep->num]->DIEPCTL, depctl.d32);

    if (ep->type == EP_TYPE_ISOC)
    {
      USB_OTG_WritePacket(pdev, ep->xfer_buff, ep->num, ep->xfer_len);   
    }    
  }
  else
  {
    /* OUT endpoint */
    depctl.d32  = USB_OTG_READ_REG32(&(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL));
    deptsiz.d32 = USB_OTG_READ_REG32(&(pdev->regs.OUTEP_REGS[ep->num]->DOEPTSIZ));
    /* Program the transfer size and packet count as follows:
    * pktcnt = N
    * xfersize = N * maxpacket
    */
    if (ep->xfer_len == 0)
    {
      deptsiz.b.xfersize = ep->maxpacket;
      deptsiz.b.pktcnt = 1;
    }
    else
    {
      deptsiz.b.pktcnt = (ep->xfer_len + (ep->maxpacket - 1)) / ep->maxpacket;
      deptsiz.b.xfersize = deptsiz.b.pktcnt * ep->maxpacket;
    }
    USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPTSIZ, deptsiz.d32);
    
    if (pdev->cfg.dma_enable == 1)
    {
      USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPDMA, ep->dma_addr);
    }
    
    if (ep->type == EP_TYPE_ISOC)
    {
      if (ep->even_odd_frame)
      {
        depctl.b.setd1pid = 1;
      }
      else
      {
        depctl.b.setd0pid = 1;
      }
    }
    /* EP enable */
    depctl.b.cnak = 1;
    depctl.b.epena = 1;
    USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL, depctl.d32);
  }
  return status;
}


/**
* @brief  USB_OTG_EP0StartXfer : Handle the setup for a data xfer for EP0 and 
*         starts the xfer
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EP0StartXfer(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS                 status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef      depctl;
  USB_OTG_DEP0XFRSIZ_TypeDef  deptsiz;
  USB_OTG_INEPREGS          *in_regs;
  uint32_t fifoemptymsk = 0;
  
  depctl.d32   = 0;
  deptsiz.d32  = 0;
  /* IN endpoint */
  if (ep->is_in == 1)
  {
    in_regs = pdev->regs.INEP_REGS[0];
    depctl.d32  = USB_OTG_READ_REG32(&in_regs->DIEPCTL);
    deptsiz.d32 = USB_OTG_READ_REG32(&in_regs->DIEPTSIZ);
    /* Zero Length Packet? */
    if (ep->xfer_len == 0)
    {
      deptsiz.b.xfersize = 0;
      deptsiz.b.pktcnt = 1;
      
    }
    else
    {
      if (ep->xfer_len > ep->maxpacket)
      {
        ep->xfer_len = ep->maxpacket;
        deptsiz.b.xfersize = ep->maxpacket;
      }
      else
      {
        deptsiz.b.xfersize = ep->xfer_len;
      }
      deptsiz.b.pktcnt = 1;
    }
    USB_OTG_WRITE_REG32(&in_regs->DIEPTSIZ, deptsiz.d32);
    
    if (pdev->cfg.dma_enable == 1)
    {
      USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[ep->num]->DIEPDMA, ep->dma_addr);  
    }
    
    /* EP enable, IN data in FIFO */
    depctl.b.cnak = 1;
    depctl.b.epena = 1;
    USB_OTG_WRITE_REG32(&in_regs->DIEPCTL, depctl.d32);
    
    
    
    if (pdev->cfg.dma_enable == 0)
    {
      /* Enable the Tx FIFO Empty Interrupt for this EP */
      if (ep->xfer_len > 0)
      {
        {
          fifoemptymsk |= 1 << ep->num;
          USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DIEPEMPMSK, 0, fifoemptymsk);
        }
      }
    }
  }
  else
  {
    /* OUT endpoint */
    depctl.d32  = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL);
    deptsiz.d32 = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPTSIZ);
    /* Program the transfer size and packet count as follows:
    * xfersize = N * (maxpacket + 4 - (maxpacket % 4))
    * pktcnt = N           */
    if (ep->xfer_len == 0)
    {
      deptsiz.b.xfersize = ep->maxpacket;
      deptsiz.b.pktcnt = 1;
    }
    else
    {
      ep->xfer_len = ep->maxpacket;
      deptsiz.b.xfersize = ep->maxpacket;
      deptsiz.b.pktcnt = 1;
    }
    USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPTSIZ, deptsiz.d32);
    if (pdev->cfg.dma_enable == 1)
    {
      USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[ep->num]->DOEPDMA, ep->dma_addr);
    }
    /* EP enable */
    depctl.b.cnak = 1;
    depctl.b.epena = 1;
    USB_OTG_WRITE_REG32 (&(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL), depctl.d32);
    
  }
  return status;
}


/**
* @brief  USB_OTG_EPSetStall : Set the EP STALL
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EPSetStall(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef  depctl;
  __IO uint32_t *depctl_addr;
  
  depctl.d32 = 0;
  if (ep->is_in == 1)
  {
    depctl_addr = &(pdev->regs.INEP_REGS[ep->num]->DIEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
    /* set the disable and stall bits */
    if (depctl.b.epena)
    {
      depctl.b.epdis = 1;
    }
    depctl.b.stall = 1;
    USB_OTG_WRITE_REG32(depctl_addr, depctl.d32);
  }
  else
  {
    depctl_addr = &(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
    /* set the stall bit */
    depctl.b.stall = 1;
    USB_OTG_WRITE_REG32(depctl_addr, depctl.d32);
  }
  return status;
}


/**
* @brief  Clear the EP STALL
* @param  pdev : Selected device
* @retval USB_OTG_STS : status
*/
USB_OTG_STS USB_OTG_EPClearStall(USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep)
{
  USB_OTG_STS status = USB_OTG_OK;
  USB_OTG_DEPCTL_TypeDef  depctl;
  __IO uint32_t *depctl_addr;
  
  depctl.d32 = 0;
  
  if (ep->is_in == 1)
  {
    depctl_addr = &(pdev->regs.INEP_REGS[ep->num]->DIEPCTL);
  }
  else
  {
    depctl_addr = &(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL);
  }
  depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
  /* clear the stall bits */
  depctl.b.stall = 0;
  if (ep->type == EP_TYPE_INTR || ep->type == EP_TYPE_BULK)
  {
    depctl.b.setd0pid = 1; /* DATA0 */
  }
  USB_OTG_WRITE_REG32(depctl_addr, depctl.d32);
  return status;
}


/**
* @brief  USB_OTG_ReadDevAllOutEp_itr : returns OUT endpoint interrupt bits
* @param  pdev : Selected device
* @retval OUT endpoint interrupt bits
*/
uint32_t USB_OTG_ReadDevAllOutEp_itr(USB_OTG_CORE_HANDLE *pdev)
{
  uint32_t v;
  v  = USB_OTG_READ_REG32(&pdev->regs.DREGS->DAINT);
  v &= USB_OTG_READ_REG32(&pdev->regs.DREGS->DAINTMSK);
  return ((v & 0xffff0000) >> 16);
}


/**
* @brief  USB_OTG_ReadDevOutEP_itr : returns Device OUT EP Interrupt register
* @param  pdev : Selected device
* @param  ep : end point number
* @retval Device OUT EP Interrupt register
*/
uint32_t USB_OTG_ReadDevOutEP_itr(USB_OTG_CORE_HANDLE *pdev , uint8_t epnum)
{
  uint32_t v;
  v  = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[epnum]->DOEPINT);
  v &= USB_OTG_READ_REG32(&pdev->regs.DREGS->DOEPMSK);
  return v;
}


/**
* @brief  USB_OTG_ReadDevAllInEPItr : Get int status register
* @param  pdev : Selected device
* @retval int status register
*/
uint32_t USB_OTG_ReadDevAllInEPItr(USB_OTG_CORE_HANDLE *pdev)
{
  uint32_t v;
  v = USB_OTG_READ_REG32(&pdev->regs.DREGS->DAINT);
  v &= USB_OTG_READ_REG32(&pdev->regs.DREGS->DAINTMSK);
  return (v & 0xffff);
}

/**
* @brief  configures EPO to receive SETUP packets
* @param  None
* @retval : None
*/
void USB_OTG_EP0_OutStart(USB_OTG_CORE_HANDLE *pdev)
{
  USB_OTG_DEP0XFRSIZ_TypeDef  doeptsize0;
  doeptsize0.d32 = 0;
  doeptsize0.b.supcnt = 3;
  doeptsize0.b.pktcnt = 1;
  doeptsize0.b.xfersize = 8 * 3;
  USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[0]->DOEPTSIZ, doeptsize0.d32 );
  
  if (pdev->cfg.dma_enable == 1)
  {
    USB_OTG_DEPCTL_TypeDef  doepctl;
    doepctl.d32 = 0;
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[0]->DOEPDMA, 
                        (uint32_t)&pdev->dev.setup_packet);
    
    /* EP enable */
    doepctl.d32 = USB_OTG_READ_REG32(&pdev->regs.OUTEP_REGS[0]->DOEPCTL);
    doepctl.b.epena = 1;
    doepctl.d32 = 0x80008000;
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[0]->DOEPCTL, doepctl.d32);
  }
}

/**
* @brief  USB_OTG_RemoteWakeup : active remote wakeup signalling
* @param  None
* @retval : None
*/
void USB_OTG_ActiveRemoteWakeup(USB_OTG_CORE_HANDLE *pdev)
{
  
  USB_OTG_DCTL_TypeDef     dctl;
  USB_OTG_DSTS_TypeDef     dsts;
  USB_OTG_PCGCCTL_TypeDef  power;  
  
  if (pdev->dev.DevRemoteWakeup) 
  {
    dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
    if(dsts.b.suspsts == 1)
    {
      if(pdev->cfg.low_power)
      {
        /* un-gate USB Core clock */
        power.d32 = USB_OTG_READ_REG32(&pdev->regs.PCGCCTL);
        power.b.gatehclk = 0;
        power.b.stoppclk = 0;
        USB_OTG_WRITE_REG32(pdev->regs.PCGCCTL, power.d32);
      }   
      /* active Remote wakeup signaling */
      dctl.d32 = 0;
      dctl.b.rmtwkupsig = 1;
      USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DCTL, 0, dctl.d32);
      USB_OTG_BSP_mDelay(5);
      USB_OTG_MODIFY_REG32(&pdev->regs.DREGS->DCTL, dctl.d32, 0 );
    }
  }
}


/**
* @brief  USB_OTG_UngateClock : active USB Core clock
* @param  None
* @retval : None
*/
void USB_OTG_UngateClock(USB_OTG_CORE_HANDLE *pdev)
{
  if(pdev->cfg.low_power)
  {
    
    USB_OTG_DSTS_TypeDef     dsts;
    USB_OTG_PCGCCTL_TypeDef  power; 
    
    dsts.d32 = USB_OTG_READ_REG32(&pdev->regs.DREGS->DSTS);
    
    if(dsts.b.suspsts == 1)
    {
      /* un-gate USB Core clock */
      power.d32 = USB_OTG_READ_REG32(&pdev->regs.PCGCCTL);
      power.b.gatehclk = 0;
      power.b.stoppclk = 0;
      USB_OTG_WRITE_REG32(pdev->regs.PCGCCTL, power.d32);
      
    }
  }
}

/**
* @brief  Stop the device and clean up fifo's
* @param  None
* @retval : None
*/
void USB_OTG_StopDevice(USB_OTG_CORE_HANDLE *pdev)
{
  uint32_t i;
  
  pdev->dev.device_status = 1;
    
  for (i = 0; i < pdev->cfg.dev_endpoints ; i++)
  {
    USB_OTG_WRITE_REG32( &pdev->regs.INEP_REGS[i]->DIEPINT, 0xFF);
    USB_OTG_WRITE_REG32( &pdev->regs.OUTEP_REGS[i]->DOEPINT, 0xFF);
  }

  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DIEPMSK, 0 );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DOEPMSK, 0 );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINTMSK, 0 );
  USB_OTG_WRITE_REG32( &pdev->regs.DREGS->DAINT, 0xFFFFFFFF );  
  
  /* Flush the FIFO */
  USB_OTG_FlushRxFifo(pdev);
  USB_OTG_FlushTxFifo(pdev ,  0x10 );  
}

/**
* @brief  returns the EP Status
* @param  pdev : Selected device
*         ep : endpoint structure
* @retval : EP status
*/

uint32_t USB_OTG_GetEPStatus(USB_OTG_CORE_HANDLE *pdev ,USB_OTG_EP *ep)
{
  USB_OTG_DEPCTL_TypeDef  depctl;
  __IO uint32_t *depctl_addr;
  uint32_t Status = 0;  
  
  depctl.d32 = 0;
  if (ep->is_in == 1)
  {
    depctl_addr = &(pdev->regs.INEP_REGS[ep->num]->DIEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
    
    if (depctl.b.stall == 1)  
      Status = USB_OTG_EP_TX_STALL;
    else if (depctl.b.naksts == 1)
      Status = USB_OTG_EP_TX_NAK;
    else 
      Status = USB_OTG_EP_TX_VALID;     

  }
  else
  {
    depctl_addr = &(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
    if (depctl.b.stall == 1)  
      Status = USB_OTG_EP_RX_STALL;
    else if (depctl.b.naksts == 1)
      Status = USB_OTG_EP_RX_NAK;
    else 
      Status = USB_OTG_EP_RX_VALID; 
  } 
  
  /* Return the current status */
  return Status;
}

/**
* @brief  Set the EP Status
* @param  pdev : Selected device
*         Status : new Status
*         ep : EP structure
* @retval : None
*/
void USB_OTG_SetEPStatus (USB_OTG_CORE_HANDLE *pdev , USB_OTG_EP *ep , uint32_t Status)
{
  USB_OTG_DEPCTL_TypeDef  depctl;
  __IO uint32_t *depctl_addr;
  
  depctl.d32 = 0;

  /* Process for IN endpoint */
  if (ep->is_in == 1)
  {
    depctl_addr = &(pdev->regs.INEP_REGS[ep->num]->DIEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);
    
    if (Status == USB_OTG_EP_TX_STALL)  
    {
      USB_OTG_EPSetStall(pdev, ep); return;
    }
    else if (Status == USB_OTG_EP_TX_NAK)
      depctl.b.snak = 1;
    else if (Status == USB_OTG_EP_TX_VALID)
    {
      if (depctl.b.stall == 1)
      {  
        ep->even_odd_frame = 0;
        USB_OTG_EPClearStall(pdev, ep);
        return;
      }      
      depctl.b.cnak = 1;
      depctl.b.usbactep = 1; 
      depctl.b.epena = 1;
    }
    else if (Status == USB_OTG_EP_TX_DIS)
      depctl.b.usbactep = 0;
  } 
  else /* Process for OUT endpoint */
  {
    depctl_addr = &(pdev->regs.OUTEP_REGS[ep->num]->DOEPCTL);
    depctl.d32 = USB_OTG_READ_REG32(depctl_addr);    
    
    if (Status == USB_OTG_EP_RX_STALL)  {
      depctl.b.stall = 1;
    }
    else if (Status == USB_OTG_EP_RX_NAK)
      depctl.b.snak = 1;
    else if (Status == USB_OTG_EP_RX_VALID)
    {
      if (depctl.b.stall == 1)
      {  
        ep->even_odd_frame = 0;
        USB_OTG_EPClearStall(pdev, ep);
        return;
      }  
      depctl.b.cnak = 1;
      depctl.b.usbactep = 1;    
      depctl.b.epena = 1;
    }
    else if (Status == USB_OTG_EP_RX_DIS)
    {
      depctl.b.usbactep = 0;    
    }
  }

  USB_OTG_WRITE_REG32(depctl_addr, depctl.d32); 
}

#endif
/**
* @}
*/ 

/**
* @}
*/ 

/**
* @}
*/

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
