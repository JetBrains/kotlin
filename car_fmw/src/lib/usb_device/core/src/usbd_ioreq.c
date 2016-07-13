/**
  ******************************************************************************
  * @file    usbd_ioreq.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    22-July-2011
  * @brief   This file provides the IO requests APIs for control endpoints.
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
#include "usbd_ioreq.h"
/** @addtogroup STM32_USB_OTG_DEVICE_LIBRARY
  * @{
  */


/** @defgroup USBD_IOREQ 
  * @brief control I/O requests module
  * @{
  */ 

/** @defgroup USBD_IOREQ_Private_TypesDefinitions
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Private_Defines
  * @{
  */ 

/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Private_Macros
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Private_Variables
  * @{
  */ 

/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Private_FunctionPrototypes
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Private_Functions
  * @{
  */ 

/**
* @brief  USBD_CtlSendData
*         send data on the ctl pipe
* @param  pdev: device instance
* @param  buff: pointer to data buffer
* @param  len: length of data to be sent
* @retval status
*/
USBD_Status  USBD_CtlSendData (USB_OTG_CORE_HANDLE  *pdev, 
                               uint8_t *pbuf,
                               uint16_t len)
{
  USBD_Status ret = USBD_OK;
  
  pdev->dev.in_ep[0].total_data_len = len;
  pdev->dev.in_ep[0].rem_data_len   = len;
  pdev->dev.device_state = USB_OTG_EP0_DATA_IN;

  DCD_EP_Tx (pdev, 0, pbuf, len);
 
  return ret;
}

/**
* @brief  USBD_CtlContinueSendData
*         continue sending data on the ctl pipe
* @param  pdev: device instance
* @param  buff: pointer to data buffer
* @param  len: length of data to be sent
* @retval status
*/
USBD_Status  USBD_CtlContinueSendData (USB_OTG_CORE_HANDLE  *pdev, 
                                       uint8_t *pbuf,
                                       uint16_t len)
{
  USBD_Status ret = USBD_OK;
  
  DCD_EP_Tx (pdev, 0, pbuf, len);
  
  
  return ret;
}

/**
* @brief  USBD_CtlPrepareRx
*         receive data on the ctl pipe
* @param  pdev: USB OTG device instance
* @param  buff: pointer to data buffer
* @param  len: length of data to be received
* @retval status
*/
USBD_Status  USBD_CtlPrepareRx (USB_OTG_CORE_HANDLE  *pdev,
                                  uint8_t *pbuf,                                  
                                  uint16_t len)
{
  USBD_Status ret = USBD_OK;
  
  pdev->dev.out_ep[0].total_data_len = len;
  pdev->dev.out_ep[0].rem_data_len   = len;
  pdev->dev.device_state = USB_OTG_EP0_DATA_OUT;
  
  DCD_EP_PrepareRx (pdev,
                    0,
                    pbuf,
                    len);
  

  return ret;
}

/**
* @brief  USBD_CtlContinueRx
*         continue receive data on the ctl pipe
* @param  pdev: USB OTG device instance
* @param  buff: pointer to data buffer
* @param  len: length of data to be received
* @retval status
*/
USBD_Status  USBD_CtlContinueRx (USB_OTG_CORE_HANDLE  *pdev, 
                                          uint8_t *pbuf,                                          
                                          uint16_t len)
{
  USBD_Status ret = USBD_OK;
  
  DCD_EP_PrepareRx (pdev,
                    0,                     
                    pbuf,                         
                    len);
  return ret;
}
/**
* @brief  USBD_CtlSendStatus
*         send zero lzngth packet on the ctl pipe
* @param  pdev: USB OTG device instance
* @retval status
*/
USBD_Status  USBD_CtlSendStatus (USB_OTG_CORE_HANDLE  *pdev)
{
  USBD_Status ret = USBD_OK;
  pdev->dev.device_state = USB_OTG_EP0_STATUS_IN;
  DCD_EP_Tx (pdev,
             0,
             NULL, 
             0); 
  
  USB_OTG_EP0_OutStart(pdev);  
  
  return ret;
}

/**
* @brief  USBD_CtlReceiveStatus
*         receive zero lzngth packet on the ctl pipe
* @param  pdev: USB OTG device instance
* @retval status
*/
USBD_Status  USBD_CtlReceiveStatus (USB_OTG_CORE_HANDLE  *pdev)
{
  USBD_Status ret = USBD_OK;
  pdev->dev.device_state = USB_OTG_EP0_STATUS_OUT;  
  DCD_EP_PrepareRx ( pdev,
                    0,
                    NULL,
                    0);  

  USB_OTG_EP0_OutStart(pdev);
  
  return ret;
}


/**
* @brief  USBD_GetRxCount
*         returns the received data length
* @param  pdev: USB OTG device instance
*         epnum: endpoint index
* @retval Rx Data blength
*/
uint16_t  USBD_GetRxCount (USB_OTG_CORE_HANDLE  *pdev , uint8_t epnum)
{
  return pdev->dev.out_ep[epnum].xfer_count;
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

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
